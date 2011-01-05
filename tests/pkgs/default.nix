{pkgs}:

with pkgs;

rec {
  testService1 = import ./testService1.nix {
    inherit stdenv;
  };
    
  testService2 = import ./testService2.nix {
    inherit stdenv;
  };
  
  testService3 = import ./testService3.nix {
    inherit stdenv;
  };
}
