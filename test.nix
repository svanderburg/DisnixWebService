rec {
    pkgs = import (builtins.getEnv "NIXPKGS_ALL") { };
    
    infrastructure = import ./infrastructure.nix;
    
    components = import ./components.nix;

    distribution = import ./distribution.nix {
	inherit components infrastructure;
    };

    export = import ./export.nix {
	inherit distribution;
	inherit (pkgs) stdenv;
    };
}
