rec {
    pkgs = import /home/sander/nix/nixpkgs/trunk/pkgs/top-level/all-packages.nix { };
    
    infrastructure = import ./infrastructure.nix;
    
    compositions = import ./compositions.nix;

    distribution = import ./distribution.nix {
	inherit compositions infrastructure;
    };

    export = import ./export.nix {
	inherit distribution;
	inherit (pkgs) stdenv;
    };
}
