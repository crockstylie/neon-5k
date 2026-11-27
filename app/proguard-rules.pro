# Règles R8 complémentaires — Neon 5K (sans packageScope partiel, cf. ADR-010)

# Conserve la signature d'androidx.activity.EdgeToEdge.enable afin que l'analyseur statique
# de la Google Play Console (API 35+) détecte l'activation explicite du bord à bord.
-keep class androidx.activity.EdgeToEdge {
    public static *** enable*(...);
}
