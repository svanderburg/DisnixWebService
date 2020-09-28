{ nixpkgs, dysnomia, disnix, DisnixWebService }:

with import "${nixpkgs}/nixos/lib/testing-python.nix" { system = builtins.currentSystem; };

simpleTest {
  nodes = {
    server =
      {pkgs, config, ...}:

      {
        imports = [ ./disnix-module.nix ../disnixwebservice-module.nix ];

        virtualisation.writableStore = true;

        networking.firewall.allowedTCPPorts = [ 22 80 ];

        services.disnixTest.enable = true;
        services.disnixTest.package = disnix;
        services.disnixTest.dysnomia = dysnomia;
        services.disnixWebServiceTest.enable = true;
        services.disnixWebServiceTest.package = DisnixWebService;

        services.httpd.enable = true;
        services.httpd.adminAddr = "admin@localhost";
        services.httpd.virtualHosts.localhost.extraConfig = ''
          <Proxy *>
            Order deny,allow
            Allow from all
            AuthType basic
            AuthName "DisnixWebService"
            AuthBasicProvider file
            AuthUserFile ${./auth/passwd}
            Require user admin
          </Proxy>

          ProxyRequests     off
          ProxyPreserveHost on
          ProxyPass         /    http://localhost:8080/ retry=5 disablereuse=on
          ProxyPassReverse  /    http://localhost:8080/
        '';

        # We can't download any substitutes in a test environment. To make tests
        # faster, we disable substitutes so that Nix does not waste any time by
        # attempting to download them.
        nix.extraOptions = ''
          substitute = false
        '';

        environment.systemPackages = [ pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
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

        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ]  ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
      };
  };
  testScript =
    ''
      start_all()

      # Wait until tomcat is started and the DisnixWebService is activated
      server.wait_for_unit("tomcat")
      server.wait_for_file("/var/tomcat/webapps/DisnixWebService")
      server.succeed("sleep 10")

      # Check authorization. The following operation should fail, since
      # we're not authorized.
      client.fail(
          "disnix-soap-client --target http://server/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}"
      )

      # Check authorization. The following operation should succeed,
      # since we're properly authorized.
      client.succeed(
          "DISNIX_SOAP_CLIENT_USERNAME=admin DISNIX_SOAP_CLIENT_PASSWORD=secret disnix-soap-client --target http://server/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}"
      )
    '';
}
