{nixpkgs, stdenv, dysnomia, disnix, DisnixWebService}:

let
  manifestTests = ./manifest;
  wrapper = import ./snapshots/wrapper.nix { inherit stdenv dysnomia; } {};
in
with import "${nixpkgs}/nixos/lib/testing-python.nix" { system = builtins.currentSystem; };

simpleTest {
  name = "snapshots";
  nodes = {
    server =
      {pkgs, config, ...}:

      {
        imports = [ ./disnix-module.nix ../disnixwebservice-module.nix ];

        virtualisation.writableStore = true;

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

        environment.systemPackages = [ pkgs.stdenv ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
      };

    client =
      {pkgs, config, ...}:

      {
        virtualisation.writableStore = true;

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ pkgs.stdenv dysnomia disnix DisnixWebService ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
      };
  };
  testScript =
    ''
      start_all()

      #### Test disnix-soap-client's snapshot operations

      server.wait_for_unit("tomcat")
      client.succeed("sleep 10")  # !!! Delay hack

      # Activate the wrapper component
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --type wrapper ${wrapper}"
      )

      # Take a snapshot
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}"
      )

      # Make a change and take another snapshot
      server.succeed("echo 1 > /var/db/wrapper/state")
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}"
      )

      # Make another change and take yet another snapshot
      server.succeed("echo 2 > /var/db/wrapper/state")
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}"
      )

      # Query all snapshots. We expect three of them.
      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l"
      )

      if int(result) == 3:
          print("We have 3 snapshots!")
      else:
          raise Exception("Expecting 3 snapshots, but we have {}!".format(result))

      # Query latest snapshot. The resulting snapshot text must contain 2.
      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-latest-snapshot --container wrapper --component ${wrapper}"
      )
      lastSnapshot = result[:-1]
      lastResolvedSnapshot = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --resolve-snapshots {}".format(
              lastSnapshot
          )
      )
      result = server.succeed("cat {}/state".format(lastResolvedSnapshot[:-1]))

      if result == "2\n":
          print("Result is 2")
      else:
          raise Exception("Result should be 2!")

      # Print missing snapshots. The former path should exist while the latter
      # should not, so it should return only one snapshot.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-missing-snapshots --container wrapper --component ${wrapper} {} wrapper/wrapper/foo | wc -l".format(
              lastSnapshot
          )
      )

      if int(result) == 1:
          print("Result is 1")
      else:
          raise Exception("Result should be 1!")

      # Export snapshot. This operation should fetch the latest snapshot from
      # the server, containing the string 2.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export-snapshots {}".format(
              lastResolvedSnapshot[:-1]
          )
      )
      tmpDir = client.succeed(
          "echo {result}/$(basename {lastSnapshot})".format(
              result=result[:-1], lastSnapshot=lastSnapshot
          )
      )
      result = client.succeed("cat {}/state".format(tmpDir[:-1]))

      if result == "2\n":
          print("Result is 2")
      else:
          raise Exception("Result should be 2!")

      # Make another change and take yet another snapshot
      server.succeed("echo 3 > /var/db/wrapper/state")
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --snapshot --type wrapper ${wrapper}"
      )

      # Run the garbage collector. After running it only one snapshot should
      # be left containing the string 3
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --clean-snapshots --container wrapper --component ${wrapper}"
      )
      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l"
      )

      if int(result) == 1:
          print("We only 1 snapshot left!")
      else:
          raise Exception("Expecting 1 remaining snapshot!")

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-latest-snapshot --container wrapper --component ${wrapper}"
      )
      lastSnapshot = result[:-1]
      lastResolvedSnapshot = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --resolve-snapshots {}".format(
              lastSnapshot
          )
      )
      result = server.succeed("cat {}/state".format(lastResolvedSnapshot[:-1]))

      if result == "3\n":
          print("Result is 3")
      else:
          raise Exception("Result should be 3!")

      # Import the snapshot that has been previously exported and check if
      # there are actually two snapshots present.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import-snapshots --localfile --container wrapper --component wrapper {}".format(
              tmpDir[:-1]
          )
      )

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l"
      )

      if int(result) == 2:
          print("Result is 2")
      else:
          raise Exception("Result should be 2!")

      # Make another change
      server.succeed("echo 4 > /var/db/wrapper/state")

      # Restore the last snapshot and check whether it has the previously
      # uploaded state (2)
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --restore --type wrapper ${wrapper}"
      )
      result = server.succeed("cat /var/db/wrapper/state")

      if int(result) == 2:
          print("Result is 2")
      else:
          raise Exception("Result should be 2!")

      # Deactivate the component
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --type wrapper ${wrapper}"
      )

      # Delete the state and check if it is not present anymore.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --delete-state --type wrapper ${wrapper}"
      )
      server.succeed("[ ! -e /var/db/wrapper ]")

      # Delete all the snapshots on the server machine and check if none is
      # present.

      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --clean-snapshots --container wrapper --component ${wrapper} --keep 0"
      )
      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-all-snapshots --container wrapper --component ${wrapper} | wc -l"
      )

      if int(result) == 0:
          print("We have no remaining snapshots left!")
      else:
          raise Exception("Expecting no remaining snapshots!")
    '';
}
