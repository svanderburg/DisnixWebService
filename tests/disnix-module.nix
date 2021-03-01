{pkgs, lib, config, ...}:

with lib;

let
  cfg = config.services.disnixTest;
in
{
  options = {
    services = {
      disnixTest = {
        enable = mkOption {
          type = types.bool;
          default = false;
          description = "Whether to enable Disnix";
        };

        package = mkOption {
          type = types.path;
          description = "The Disnix package";
        };

        dysnomia = mkOption {
          type = types.path;
          description = "The Dysnomia package";
        };
      };
    };
  };

  config = mkIf cfg.enable {
    services.dbus.enable = true;
    services.dbus.packages = [ cfg.package ];

    systemd.services.disnix =
      { description = "Disnix server";

        wantedBy = [ "multi-user.target" ];
        after = [ "dbus.service" ];

        path = [ pkgs.nix pkgs.getopt cfg.package cfg.dysnomia ];
        environment = {
          HOME = "/root";
        };

        serviceConfig.ExecStart = "${cfg.package}/bin/disnix-service";
      };

    ids.gids = { disnix = 200; };
    users.extraGroups = {
      disnix = { gid = 200; };
    };
    environment.variables.DISNIX_REMOTE_CLIENT = "disnix-client";
  };
}
