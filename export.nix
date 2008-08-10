{ stdenv, distribution }:

let
    distributionExport = map (entry: { service = if entry.service ? pkg then entry.service.pkg.outPath else null; target = entry.target.targetEPR; } ) distribution;
in
    
stdenv.mkDerivation {
    name = "distribution-export";
    outputXML = builtins.toXML distributionExport;
    builder = ./builder.sh;
}
