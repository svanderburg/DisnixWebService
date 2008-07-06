{pkgs ? "foo", infrastructure ? "bar"}:

[
#    { composition = pkgs.helloworld ; machine = infrastructure.test; }
    { composition = "helloworld"; machine = "kubuntuvm"; }
]
