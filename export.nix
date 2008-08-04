{ stdenv, distribution }:

let
    distributionExport = map (entry: { service = entry.service.pkg.outPath; target = entry.target.targetEPR; } ) distribution;
in
    
stdenv.mkDerivation rec {
    name = "distribution-export";
    outputXML = builtins.toXML distributionExport;
    builder = ./builder.sh;
}
