{ nixpkgs, dysnomia, disnix, DisnixWebService }:

let
  deployment = ./deployment;
in
with import "${nixpkgs}/nixos/lib/testing-python.nix" { system = builtins.currentSystem; };

simpleTest {
  nodes = {
    server =
      {pkgs, config, ...}:

      {
        imports = [ ./disnix-module.nix ../disnixwebservice-module.nix ];

        virtualisation.writableStore = true;
        virtualisation.additionalPaths = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ] ++ pkgs.coreutils.all ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

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
        imports = [ ./disnix-module.nix ];

        virtualisation.writableStore = true;
        virtualisation.additionalPaths = [ pkgs.stdenv pkgs.perlPackages.ArchiveCpio pkgs.busybox ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv ];
      };
  };
  testScript =
    ''
      start_all()

      # Wait until tomcat is started and the DisnixWebService is activated
      server.wait_for_unit("tomcat")
      server.wait_for_file("/var/tomcat/webapps/DisnixWebService")
      server.succeed("sleep 10")

      # Check invalid path. We query an invalid path from the service
      # which should return the path we have given.
      # This test should succeed.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid /nix/store/00000000000000000000000000000000-invalid"
      )

      if "/nix/store/00000000000000000000000000000000-invalid" in result:
          print("/nix/store/00000000000000000000000000000000-invalid is invalid")
      else:
          raise Exception(
              "/nix/store/00000000000000000000000000000000-invalid should be invalid"
          )

      # Check invalid path. We query a valid path from the service
      # which should return nothing in this case.
      # This test should succeed.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}"
      )

      # Query requisites test. Queries the requisites of the bash shell
      # and checks whether it is part of the closure.
      # This test should succeed.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-requisites ${pkgs.bash}"
      )

      if "bash" in result:
          print("${pkgs.bash} is in the closure")
      else:
          raise Exception(
              "${pkgs.bash} should be in the closure!"
          )

      # Realise test. First the coreutils derivation file is instantiated,
      # then it is realised. This test should succeed.

      result = server.succeed(
          "nix-instantiate ${nixpkgs} -A coreutils"
      )
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --realise {}".format(
              result
          )
      )

      # Export test. Exports the closure of the bash shell on the server
      # and then imports it on the client. This test should succeed.

      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export --remotefile ${pkgs.bash}"
      )
      client.succeed("nix-store --import < {}".format(result))

      # Export local test. Exports the local closure of the bash shell on the
      # server and then imports it on the client. This test should succeed.
      result = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --export --localfile ${pkgs.bash}"
      )
      server.succeed("nix-store --import < {}".format(result[:-1]))

      # Import test. First we create a manifest, then we take the
      # closure of the target2Profile on the client. Then it imports the
      # closure into the Nix store of the server. This test should
      # succeed.

      result = client.succeed(
          "NIX_PATH='nixpkgs=${nixpkgs}' disnix-manifest --target-property targetEPR -s ${deployment}/DistributedDeployment/services.nix -i ${deployment}/DistributedDeployment/infrastructure.nix -d ${deployment}/DistributedDeployment/distribution.nix"
      )
      manifestClosure = client.succeed("nix-store -qR {}".format(result)).split("\n")
      target2Profile = [c for c in manifestClosure if "-testTarget2" in c][0]

      server.fail("nix-store --check-validity {}".format(target2Profile))
      client.succeed(
          "nix-store --export $(nix-store -qR {}) > /root/target2Profile.closure".format(
              target2Profile
          )
      )
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import --localfile /root/target2Profile.closure"
      )
      server.succeed("nix-store --check-validity {}".format(target2Profile))

      # Import remote test. Export the closure of bash and try to import it.
      # This test should succeed.
      server.succeed("nix-store --export $(nix-store -qR /bin/sh) > /root/bash.closure")
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --import --remotefile /root/bash.closure"
      )

      # Set test. Adds the testTarget2 profile as only derivation into
      # the Disnix profile. We first set the profile, then we check
      # whether the profile is part of the closure.
      # This test should succeed.

      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --set --profile default {}".format(
              target2Profile
          )
      )
      defaultProfileClosure = server.succeed(
          "nix-store -qR /nix/var/nix/profiles/disnix/default"
      ).split("\n")

      if target2Profile in defaultProfileClosure:
          print("{} is part of the closure".format(target2Profile))
      else:
          raise Exception("{} should be part of the closure".format(target2Profile))

      # Query installed test. Queries the installed services in the
      # profile, which has been set in the previous testcase.
      # testService2 should be in there. This test should succeed.

      closure = client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --query-installed --profile default"
      )

      if "-testService2" in closure:
          print("testService2 is installed in the default profile")
      else:
          raise Exception("testService2 should be installed in the default profile")

      # Collect garbage test. This test should succeed.
      # Testcase disabled, as this is very expensive.
      # client.succeed("disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --collect-garbage")

      # Lock test. This test should succeed.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock"
      )

      # Lock test. This test should fail, since the service instance is already locked.
      client.fail(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --lock"
      )

      # Unlock test. This test should succeed, so that we can release the lock.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock"
      )

      # Unlock test. This test should fail as the lock has already been released.
      client.fail(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --unlock"
      )

      # Copy the closure of testService1 from the client to the server.
      # This test should succeed.
      testService1 = [c for c in manifestClosure if "-testService1" in c][0]
      client.succeed(
          "disnix-copy-closure --to --target http://server:8080/DisnixWebService/services/DisnixWebService --interface disnix-soap-client {}".format(
              testService1
          )
      )

      # Use the echo type to activate a service.
      # We use the testService1 service defined in the manifest.
      # This test should succeed.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --activate --arguments foo=foo --arguments bar=bar --type echo {}".format(
              testService1
          )
      )

      # Deactivate the same service using the echo type. This test should succeed.
      client.succeed(
          "disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --deactivate --arguments foo=foo --arguments bar=bar --type echo {}".format(
              testService1
          )
      )

      # Capture the remote machine's configuration and check whether the foo=bar
      # property is there.
      client.succeed(
          'disnix-soap-client --target http://server:8080/DisnixWebService/services/DisnixWebService --capture-config | grep \'"foo" = "bar"\${"'"}'
      )
    '';
}
