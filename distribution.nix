{compositions ? (import ./compositions.nix), infrastructure ? (import ./infrastructure.nix)}:

[
    { composition = compositions.helloworld; machine = infrastructure.dummy; }
]
