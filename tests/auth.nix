{ nixpkgs, dysnomia, disnix, DisnixWebService }:

with import "${nixpkgs}/nixos/lib/testing.nix" { system = builtins.currentSystem; };

simpleTest {
  nodes = {
    server =
      {pkgs, config, ...}:
      
      {
        imports = [ ../disnixwebservice-module.nix ];
        
        virtualisation.writableStore = true;
        
        networking.firewall.allowedTCPPorts = [ 22 80 ];
        
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

        services.httpd.enable = true;
        services.httpd.adminAddr = "admin@localhost";
        services.httpd.hostName = "localhost";
        services.httpd.extraConfig = ''
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
        
        environment.systemPackages = [ pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ] ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
      };
      
    client =
      {pkgs, config, ...}:
      
      {
        virtualisation.writableStore = true;
        environment.systemPackages = [ disnix DisnixWebService pkgs.stdenv pkgs.paxctl pkgs.busybox pkgs.gnumake pkgs.patchelf pkgs.gcc ]  ++ pkgs.libxml2.all ++ pkgs.libxslt.all;
      };
  };
  testScript = 
    ''
      startAll;
      
      # Wait until tomcat is started and the DisnixWebService is activated
      $server->waitForJob("tomcat");
      $server->waitForFile("/var/tomcat/webapps/DisnixWebService");
      $server->mustSucceed("sleep 10");
      
      # Check authorization. The following operation should fail, since
      # we're not authorized.
      $client->mustFail("disnix-soap-client --target http://server/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}");
      
      # Check authorization. The following operation should succeed,
      # since we're properly authorized.
      $client->mustSucceed("DISNIX_SOAP_CLIENT_USERNAME=admin DISNIX_SOAP_CLIENT_PASSWORD=secret disnix-soap-client --target http://server/DisnixWebService/services/DisnixWebService --print-invalid ${pkgs.bash}");
    '';
}
