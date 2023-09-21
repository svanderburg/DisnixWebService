{ nixpkgs, dysnomia, disnix, DisnixWebService }:

let
  deployment = ./deployment;
in
with import "${nixpkgs}/nixos/lib/testing-python.nix" { system = builtins.currentSystem; };

simpleTest {
  name = "deployment";
  nodes = {
    testTarget1 =
      {pkgs, config, ...}:

      {
        imports = [ ./disnix-module.nix ../disnixwebservice-module.nix ];

        virtualisation.writableStore = true;
        virtualisation.additionalPaths = [ pkgs.stdenv pkgs.stdenvNoCC ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        networking.firewall.allowedTCPPorts = [ 22 8080 ];

        services.disnixTest.enable = true;
        services.disnixTest.package = disnix;
        services.disnixTest.dysnomia = dysnomia;
        services.disnixWebServiceTest.enable = true;
        services.disnixWebServiceTest.package = DisnixWebService;

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
        imports = [ ./disnix-module.nix ];

        virtualisation.writableStore = true;
        virtualisation.additionalPaths = [ pkgs.stdenv pkgs.stdenvNoCC ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        services.openssh.enable = true;
        services.disnixTest.enable = true;
        services.disnixTest.package = disnix;
        services.disnixTest.dysnomia = dysnomia;

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
        virtualisation.additionalPaths = [ pkgs.stdenv pkgs.stdenvNoCC ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv pkgs.libxml2 ];
        environment.variables.DISNIX_REMOTE_CLIENT = "disnix-client";
      };
  };
  testScript =
    let
      env = "NIX_PATH='nixpkgs=${nixpkgs}' SSH_OPTS='-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no'";
    in
    ''
      import subprocess

      start_all()

      # Wait until tomcat is started and the DisnixWebService is activated
      testTarget1.wait_for_job("tomcat")
      testTarget1.wait_for_file("/var/tomcat/webapps/DisnixWebService")
      testTarget1.succeed("sleep 10")

      # Wait until SSH is running
      testTarget2.wait_for_unit("sshd")

      # Initialise ssh stuff by creating a key pair for communication
      key = subprocess.check_output(
          '${pkgs.openssh}/bin/ssh-keygen -t ecdsa -f key -N ""',
          shell=True,
      )

      testTarget2.succeed("mkdir -m 700 /root/.ssh")
      testTarget2.copy_from_host("key.pub", "/root/.ssh/authorized_keys")

      coordinator.succeed("mkdir -m 700 /root/.ssh")
      coordinator.copy_from_host("key", "/root/.ssh/id_dsa")
      coordinator.succeed("chmod 600 /root/.ssh/id_dsa")

      # Deploy the test configuration.
      # This test should succeed.
      coordinator.succeed(
          "${env} disnix-env --build-on-targets -s ${deployment}/DistributedDeployment/services.nix -i ${deployment}/DistributedDeployment/infrastructure-multiproto.nix -d ${deployment}/DistributedDeployment/distribution.nix"
      )

      # Query the installed services per machine and check if the
      # expected services are there.
      # This test should succeed.
      coordinator.succeed(
          "${env} disnix-query -f xml ${deployment}/DistributedDeployment/infrastructure-multiproto.nix > query.xml"
      )

      coordinator.succeed(
          "xmllint --xpath \"/profileManifestTargets/target[@name='testTarget1']/profileManifest/services/service[name='testService1']/name\" query.xml"
      )
      coordinator.succeed(
          "xmllint --xpath \"/profileManifestTargets/target[@name='testTarget2']/profileManifest/services/service[name='testService2']/name\" query.xml"
      )
      coordinator.succeed(
          "xmllint --xpath \"/profileManifestTargets/target[@name='testTarget2']/profileManifest/services/service[name='testService3']/name\" query.xml"
      )

      # Test disnix-reconstruct. First, we remove the old manifests. They
      # should have been reconstructed.

      coordinator.succeed(
          "${env} disnix-env --delete-all-generations"
      )
      coordinator.succeed(
          "${env} disnix-reconstruct ${deployment}/DistributedDeployment/infrastructure.nix"
      )
      result = coordinator.succeed(
          "ls /nix/var/nix/profiles/per-user/root/disnix-coordinator | wc -l"
      )

      if int(result) == 2:
          print("We have a reconstructed manifest!")
      else:
          raise Exception("We don't have any reconstructed manifests!")
    '';
}
