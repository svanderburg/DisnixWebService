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
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox pkgs.patchelf ];
        
        networking.firewall.allowedTCPPorts = [ 22 8080 ];
        
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

        services.tomcat.enable = true;
        services.tomcat.extraGroups = [ "disnix" ];
        services.tomcat.javaOpts = "-Djava.library.path=${pkgs.libmatthew_java}/lib/jni";
        services.tomcat.catalinaOpts = "-Xms64m -Xmx256m";
        services.tomcat.sharedLibs = [
          "${DisnixWebService}/share/java/DisnixConnection.jar"
          "${pkgs.dbus_java}/share/java/dbus.jar"
        ];
        services.tomcat.webapps = [ DisnixWebService ];
        
        environment.systemPackages = [ pkgs.stdenv ];
      };
    
    testTarget2 = 
      {pkgs, config, ...}:
      
      {
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ];
        
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
        
        environment.systemPackages = [ pkgs.stdenv disnix ];
      };
    
    coordinator =
      {pkgs, config, ...}:
      
      {
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ];
        
        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv ];
      };
  };
  testScript = 
    ''
      startAll;
      
      # Wait until tomcat is started and the DisnixWebService is activated
      $testTarget1->waitForJob("tomcat");
      $testTarget1->waitForFile("/var/tomcat/webapps/DisnixWebService");
      $testTarget1->mustSucceed("sleep 10");
      
      # Wait until SSH is running
      $testTarget2->waitForJob("sshd");
      
      # Initialise ssh stuff by creating a key pair for communication
      my $key=`${pkgs.openssh}/bin/ssh-keygen -t dsa -f key -N ""`;

      $testTarget2->mustSucceed("mkdir -m 700 /root/.ssh");
      $testTarget2->copyFileFromHost("key.pub", "/root/.ssh/authorized_keys");

      $coordinator->mustSucceed("mkdir -m 700 /root/.ssh");
      $coordinator->copyFileFromHost("key", "/root/.ssh/id_dsa");
      $coordinator->mustSucceed("chmod 600 /root/.ssh/id_dsa");
      
      # Deploy the test configuration.
      # This test should succeed.
      $coordinator->mustSucceed("NIX_PATH='nixpkgs=${nixpkgs}' SSH_OPTS='-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no' disnix-env --build-on-targets -s ${deployment}/DistributedDeployment/services.nix -i ${deployment}/DistributedDeployment/infrastructure-multiproto.nix -d ${deployment}/DistributedDeployment/distribution.nix");
      
      # Query the installed services per machine and check if the
      # expected services are there.
      # This test should succeed.
      my @lines = split('\n', $coordinator->mustSucceed("SSH_OPTS='-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no' disnix-query ${deployment}/DistributedDeployment/infrastructure-multiproto.nix"));
      
      if($lines[1] ne "Services on: http://testTarget1:8080/DisnixWebService/services/DisnixWebService") {
          die "disnix-query output line 1 does not match what we expect!\n";
      }

      if($lines[3] =~ /\-testService1/) {
          print "Found testService1 on disnix-query output line 3\n";
      } else {
          die "disnix-query output line 3 does not contain testService1!\n";
      }
      
      if($lines[6] ne "Services on: testTarget2") {
          die "disnix-query output line 6 does not match what we expect $lines[6]!\n";
      }
      
      if($lines[8] =~ /\-testService2/) {
          print "Found testService2 on disnix-query output line 8\n";
      } else {
          die "disnix-query output line 7 does not contain testService2!\n";
      }
      
      if($lines[9] =~ /\-testService3/) {
          print "Found testService3 on disnix-query output line 9\n";
      } else {
          die "disnix-query output line 9 does not contain testService3!\n";
      }
    '';
}
