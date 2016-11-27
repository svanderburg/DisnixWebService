{nixpkgs, stdenv, dysnomia, disnix, DisnixWebService}:

let
  manifestTests = ./manifest;
  wrapper = import ./snapshots/wrapper.nix { inherit stdenv dysnomia; } {};
in
with import "${nixpkgs}/nixos/lib/testing.nix" { system = builtins.currentSystem; };

  simpleTest {
    nodes = {
      server =
        {pkgs, config, ...}:
        
        {
          imports = [ ../disnixwebservice-module.nix ];
          
          virtualisation.writableStore = true;
          
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

            environment.systemPackages = [ pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
        };
      
      client =
        {pkgs, config, ...}:
        
        {
          virtualisation.writableStore = true;
          environment.systemPackages = [ dysnomia disnix DisnixWebService pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
        };
    };
    testScript = 
      ''
        startAll;
        
        #### Test disnix-soap-client's snapshot operations
        
        $server->waitForJob("tomcat");
        $client->mustSucceed("sleep 10"); # !!! Delay hack
        
        # Activate the wrapper component
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --type wrapper ${wrapper}");
        
        # Take a snapshot
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}");
        
        # Make a change and take another snapshot
        $server->mustSucceed("echo 1 > /var/db/wrapper/state");
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}");
        
        # Make another change and take yet another snapshot
        $server->mustSucceed("echo 2 > /var/db/wrapper/state");
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}");
        
        # Query all snapshots. We expect three of them.
        my $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l");
        
        if($result == 3) {
            print "We have 3 snapshots!\n";
        } else {
            die "Expecting 3 snapshots, but we have $result!";
        }
        
        # Query latest snapshot. The resulting snapshot text must contain 2.
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-latest-snapshot --container wrapper --component ${wrapper}");
        my $lastSnapshot = substr $result, 0, -1;
        my $lastResolvedSnapshot = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --resolve-snapshots $lastSnapshot");
        $result = $server->mustSucceed("cat ".(substr $lastResolvedSnapshot, 0, -1)."/state");
        
        if($result eq "2\n") {
            print "Result is 2\n";
        } else {
            die "Result should be 2!";
        }
        
        # Print missing snapshots. The former path should exist while the latter
        # should not, so it should return only one snapshot.
        
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-missing-snapshots --container wrapper --component ${wrapper} $lastSnapshot wrapper/wrapper/foo | wc -l");
        
        if($result == 1) {
            print "Result is 1\n";
        } else {
            die "Result should be 1!";
        }
        
        # Export snapshot. This operation should fetch the latest snapshot from
        # the server, containing the string 2.
        
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export-snapshots ".(substr $lastResolvedSnapshot, 0, -1));
        my $tmpDir = $client->mustSucceed("echo ".(substr $result, 0, -1)."/\$(basename $lastSnapshot)");
        $result = $client->mustSucceed("cat ".(substr $tmpDir, 0, -1)."/state");
        
        if($result == 2) {
            print "Result is 2\n";
        } else {
            die "Result should be 2!";
        }
        
        # Make another change and take yet another snapshot
        $server->mustSucceed("echo 3 > /var/db/wrapper/state");
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}");
        
        # Run the garbage collector. After running it only one snapshot should
        # be left containing the string 3
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --clean-snapshots --container wrapper --component ${wrapper}");
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l");
        
        if($result == 1) {
            print "We only 1 snapshot left!\n";
        } else {
            die "Expecting 1 remaining snapshot!";
        }
        
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-latest-snapshot --container wrapper --component ${wrapper}");
        $lastSnapshot = substr $result, 0, -1;
        $lastResolvedSnapshot = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --resolve-snapshots $lastSnapshot");
        $result = $server->mustSucceed("cat ".(substr $lastResolvedSnapshot, 0, -1)."/state");
        
        if($result eq "3\n") {
            print "Result is 3\n";
        } else {
            die "Result should be 3!";
        }
        
        # Import the snapshot that has been previously exported and check if
        # there are actually two snapshots present.
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import-snapshots --localfile --container wrapper --component wrapper ".(substr $tmpDir, 0, -1));
        
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l");
        
        if($result == 2) {
            print "Result is 2\n";
        } else {
            die "Result should be 2!";
        }
        
        # Make another change
        $server->mustSucceed("echo 4 > /var/db/wrapper/state");
        
        # Restore the last snapshot and check whether it has the previously
        # uploaded state (2)
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --restore --type wrapper ${wrapper}");
        $result = $server->mustSucceed("cat /var/db/wrapper/state");
        
        if($result == 2) {
            print "Result is 2\n";
        } else {
            die "Result should be 2!";
        }
        
        # Deactivate the component
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --type wrapper ${wrapper}");
        
        # Delete the state and check if it is not present anymore.
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --delete-state --type wrapper ${wrapper}");
        $server->mustSucceed("[ ! -e /var/db/wrapper ]");
        
        # Delete all the snapshots on the server machine and check if none is
        # present.
        
        $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --clean-snapshots --container wrapper --component ${wrapper} --keep 0");
        $result = $client->mustSucceed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l");
        
        if($result == 0) {
            print "We have no remaining snapshots left!\n";
        } else {
            die "Expecting no remaining snapshots!";
        }
      '';
  }
