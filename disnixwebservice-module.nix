{config, pkgs, lib, ...}:

with lib;

let
  cfg = config.services.disnixWebServiceTest;
in
{
  options = {
    services = {
      disnixWebServiceTest = {
        enable = mkOption {
          type = types.bool;
          default = false;
          description = "Whether to enable the Disnix web service";
        };

        package = mkOption {
          type = types.path;
          description = "The DisnixWebService package";
        };
      };
    };
  };

  config = mkIf cfg.enable {
    services.tomcat = {
      enable = true;
      extraGroups = [ "disnix" ];
      javaOpts = "-Djava.library.path=${pkgs.libmatthew_java}/lib/jni";
      catalinaOpts = "-Xms64m -Xmx256m";
      sharedLibs = [
        "${cfg.package}/share/java/DisnixConnection.jar"
        "${pkgs.dbus_java}/share/java/dbus.jar"
      ];
      webapps = [ cfg.package ];
    };

    services.disnixWebServiceTest.package = mkDefault (import ./release.nix {}).build."${pkgs.stdenv.system}";

    environment.systemPackages = [ cfg.package ];
  };
}
