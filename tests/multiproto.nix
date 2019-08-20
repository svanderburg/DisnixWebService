{ nixpkgs, dysnomia, disnix, DisnixWebService }:

let
  deployment = ./deployment;
in
with import "${nixpkgs}/nixos/lib/testing.nix" { system = builtins.currentSystem; };

simpleTest {
  nodes = {
    testTarget1 =
      {pkgs, config, ...}:

      {
        imports = [ ../disnixwebservice-module.nix ];

        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox pkgs.patchelf ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        networking.firewall.allowedTCPPorts = [ 22 8080 ];

        services.disnixWebServiceTest.enable = true;
        services.disnixWebServiceTest.package = DisnixWebService;

        services.dbus.enable = true;
        services.dbus.packages = [ disnix ];

        systemd.services.disnix =
          { description = "Disnix server";

            wantedBy = [ "multi-user.target" ];
            after = [ "dbus.service" ];

            path = [ pkgs.nix pkgs.getopt disnix dysnomia ];
            environment = {
              HOME = "/root";
            };

            serviceConfig.ExecStart = "${disnix}/bin/disnix-service";
          };

        ids.gids = { disnix = 200; };
        users.extraGroups = [ { gid = 200; name = "disnix"; } ];

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ pkgs.stdenv ];
      };

    testTarget2 = 
      {pkgs, config, ...}:

      {
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        services.dbus.enable = true;
        services.dbus.packages = [ disnix ];
        services.openssh.enable = true;

        systemd.services.disnix =
          { description = "Disnix server";

            wantedBy = [ "multi-user.target" ];
            after = [ "dbus.service" ];

            path = [ pkgs.nix pkgs.getopt disnix dysnomia ];
            environment = {
              HOME = "/root";
            };

            serviceConfig.ExecStart = "${disnix}/bin/disnix-service";
          };

        ids.gids = { disnix = 200; };
        users.extraGroups = [ { gid = 200; name = "disnix"; } ];

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ pkgs.stdenv disnix ];
      };

    coordinator =
      {pkgs, config, ...}:

      {
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv pkgs.libxml2 ];
      };
  };
  testScript =
    let
      env = "NIX_PATH='nixpkgs=${nixpkgs}' SSH_OPTS='-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no'";
    in
    ''
      startAll;

      # Wait until tomcat is started and the DisnixWebService is activated
      $testTarget1->waitForJob("tomcat");
      $testTarget1->waitForFile("/var/tomcat/webapps/DisnixWebService");
      $testTarget1->mustSucceed("sleep 10");

      # Wait until SSH is running
      $testTarget2->waitForJob("sshd");

      # Initialise ssh stuff by creating a key pair for communication
      my $key=`${pkgs.openssh}/bin/ssh-keygen -t ecdsa -f key -N ""`;

      $testTarget2->mustSucceed("mkdir -m 700 /root/.ssh");
      $testTarget2->copyFileFromHost("key.pub", "/root/.ssh/authorized_keys");

      $coordinator->mustSucceed("mkdir -m 700 /root/.ssh");
      $coordinator->copyFileFromHost("key", "/root/.ssh/id_dsa");
      $coordinator->mustSucceed("chmod 600 /root/.ssh/id_dsa");

      # Deploy the test configuration.
      # This test should succeed.
      $coordinator->mustSucceed("${env} disnix-env --build-on-targets -s ${deployment}/DistributedDeployment/services.nix -i ${deployment}/DistributedDeployment/infrastructure-multiproto.nix -d ${deployment}/DistributedDeployment/distribution.nix");

      # Query the installed services per machine and check if the
      # expected services are there.
      # This test should succeed.
      $coordinator->mustSucceed("${env} disnix-query -f xml ${deployment}/DistributedDeployment/infrastructure-multiproto.nix > query.xml");

      $coordinator->mustSucceed("xmllint --xpath \"/profileManifestTargets/target[\@name='http://testTarget1:8080/DisnixWebService/services/DisnixWebService']/profileManifest/services/service[name='testService1']/name\" query.xml");
      $coordinator->mustSucceed("xmllint --xpath \"/profileManifestTargets/target[\@name='testTarget2']/profileManifest/services/service[name='testService2']/name\" query.xml");
      $coordinator->mustSucceed("xmllint --xpath \"/profileManifestTargets/target[\@name='testTarget2']/profileManifest/services/service[name='testService3']/name\" query.xml");

      # Test disnix-reconstruct. First, we remove the old manifests. They
      # should have been reconstructed.

      $coordinator->mustSucceed("${env} disnix-env --delete-all-generations");
      $coordinator->mustSucceed("${env} disnix-reconstruct ${deployment}/DistributedDeployment/infrastructure.nix");
      my $result = $coordinator->mustSucceed("ls /nix/var/nix/profiles/per-user/root/disnix-coordinator | wc -l");

      if($result == 2) {
          print "We have a reconstructed manifest!\n";
      } else {
          die "We don't have any reconstructed manifests!";
      }
    '';
}
