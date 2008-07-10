rec {
    pkgs = import /home/sander/nix/nixpkgs/trunk/pkgs/top-level/all-packages.nix { };
    
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
