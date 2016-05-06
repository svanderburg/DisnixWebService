{ nixpkgs, dysnomia, disnix, DisnixWebService }:

let
  deployment = ./deployment;
in
with import "${nixpkgs}/nixos/lib/testing.nix" { system = builtins.currentSystem; };

simpleTest {
  nodes = {
    server =
      {pkgs, config, ...}:
      
      {
        virtualisation.writableStore = true;
        virtualisation.pathsInNixDB = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ];
        
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
          environment.etc."dysnomia/properties" = {
            source = pkgs.writeTextFile {
              name = "dysnomia-properties";
              text = ''
                foo=bar
              '';
            };
          };
      };
      
    client =
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
      $server->waitForJob("tomcat");
      $server->waitForFile("/var/tomcat/webapps/DisnixWebService");
      $server->mustSucceed("sleep 10");
      
      # Check invalid path. We query an invalid path from the service
      # which should return the path we have given.
      # This test should succeed.
      
      my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid /nix/store/invalid");
      
      if($result =~ /\/nix\/store\/invalid/) {
          print "/nix/store/invalid is invalid\n";
      } else {
          die "/nix/store/invalid should be invalid\n";
      }
      
      # Check invalid path. We query a valid path from the service
      # which should return nothing in this case.
      # This test should succeed.
      
      $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}");
      
      # Query requisites test. Queries the requisites of the bash shell
      # and checks whether it is part of the closure.
      # This test should succeed.
      
      $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-requisites ${pkgs.bash}");
      
      if($result =~ /bash/) {
          print "${pkgs.bash} is in the closure\n";
      } else {
          die "${pkgs.bash} should be in the closure!\n";
      }
      
      # Realise test. First the coreutils derivation file is instantiated,
      # then it is realised. This test should succeed.
      
      $result = $server->mustSucceed("nix-instantiate ${nixpkgs} -A coreutils");
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --realise $result");
      
      # Export test. Exports the closure of the bash shell on the server
      # and then imports it on the client. This test should succeed.
      
      $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export --remotefile ${pkgs.bash}");
      $client->mustSucceed("nix-store --import < $result");
      
      # Export local test. Exports the local closure of the bash shell on the
      # server and then imports it on the client. This test should succeed.
      $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export --localfile ${pkgs.bash}");
      $server->mustSucceed("nix-store --import < ".substr $result, 0, -1);
      
      # Import test. First we create a manifest, then we take the
      # closure of the target2Profile on the client. Then it imports the
      # closure into the Nix store of the server. This test should
      # succeed.
      
      $result = $client->mustSucceed("NIX_PATH='nixpkgs=${nixpkgs}' disnix-manifest --target-property targetEPR -s ${deployment}/DistributedDeployment/services.nix -i ${deployment}/DistributedDeployment/infrastructure.nix -d ${deployment}/DistributedDeployment/distribution.nix");
      my @manifestClosure = split('\n', $client->mustSucceed("nix-store -qR $result"));
      my @target2Profile = grep(/\-testTarget2/, @manifestClosure);
      
      $server->mustFail("nix-store --check-validity @target2Profile");
      $client->mustSucceed("nix-store --export \$(nix-store -qR @target2Profile) > /root/target2Profile.closure");
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import --localfile /root/target2Profile.closure");
      $server->mustSucceed("nix-store --check-validity @target2Profile");
      
      # Import remote test. Export the closure of bash and try to import it.
      # This test should succeed.
      $server->mustSucceed("nix-store --export \$(nix-store -qR /bin/sh) > /root/bash.closure");
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import --remotefile /root/bash.closure");
      
      # Set test. Adds the testTarget2 profile as only derivation into 
      # the Disnix profile. We first set the profile, then we check
      # whether the profile is part of the closure.
      # This test should succeed.
      
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --set --profile default @target2Profile");
      my @defaultProfileClosure = split('\n', $server->mustSucceed("nix-store -qR /nix/var/nix/profiles/disnix/default"));
      my @closure = grep("@target2Profile", @defaultProfileClosure);
      
      if("@closure" eq "") {
          die "@target2Profile should be part of the closure\n";
      } else {
          print "@target2Profile is part of the closure\n";
      }
      
      # Query installed test. Queries the installed services in the 
      # profile, which has been set in the previous testcase.
      # testService2 should be in there. This test should succeed.
      
      @closure = split('\n', $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-installed --profile default"));
      my @service = grep(/\-testService2/, @closure);
      
      if("@service" eq "") {
          die "@service should be installed in the default profile\n";
      } else {
          print "@service is installed in the default profile\n";
      }
      
      # Collect garbage test. This test should succeed.
      # Testcase disabled, as this is very expensive.
      #$client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --collect-garbage");

      # Lock test. This test should succeed.
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
      
      # Lock test. This test should fail, since the service instance is already locked.
      $client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock");
      
      # Unlock test. This test should succeed, so that we can release the lock.
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
      
      # Unlock test. This test should fail as the lock has already been released.
      $client->mustFail("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock");
      
      # Copy the closure of testService1 from the client to the server.
      # This test should succeed.
      my @testService1 = grep(/\-testService1/, @manifestClosure);
      $client->mustSucceed("disnix-copy-closure --to --target http://server:8080/DisnixWebService/services/DisnixWebService --interface disnix-soap-client @testService1");
      
      # Use the echo type to activate a service.
      # We use the testService1 service defined in the manifest.
      # This test should succeed.
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --arguments foo=foo --arguments bar=bar --type echo @testService1");
      
      # Deactivate the same service using the echo type. This test should succeed.
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --arguments foo=foo --arguments bar=bar --type echo @testService1");
      
      # Capture the remote machine's configuration and check whether the foo=bar
      # property is there.
      $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --capture-config | grep '\"foo\" = \"bar\"'");
    '';
}
