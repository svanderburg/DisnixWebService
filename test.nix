rec {
    pkgs = import (builtins.getEnv "NIXPKGS_ALL") { };
    
    infrastructure = import ./infrastructure.nix;
    
    services = import ./services.nix;

    distribution = import ./distribution.nix {
	inherit services infrastructure;
    };

    export = import ./export.nix {
	inherit distribution;
	inherit (pkgs) stdenv;
    };
}
