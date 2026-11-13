"""Typeset chapter markers using Wombatcraft's existing in-game display font."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "pack/kubejs/assets/wombatcraft/textures/questbook"
PALETTES = {
    "frontier": ("#34273d", "#cda9e4", "Wombatcraft"),
    "workshop": ("#352720", "#e6b47e", "Create"),
    "network": ("#29273e", "#c2b6ef", "Applied Energistics"),
    "arcane": ("#302643", "#d6b7fb", "Ars Nouveau"),
    "twilight": ("#202e2e", "#adceab", "Twilight Forest"),
    "space": ("#222d45", "#b5d5ff", "Ad Astra"),
    "convergence": ("#36283d", "#e8c88d", "The Convergence"),
}


def build():
    DEST.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype(str(ROOT / "branding/Cinzel.ttf"), 44)
    for key, (background, ink, title) in PALETTES.items():
        image = Image.new("RGBA", (600, 100))
        draw = ImageDraw.Draw(image)
        draw.polygon([(0, 50), (32, 12), (568, 12), (599, 50), (568, 88), (32, 88)],
                     fill=background, outline=ink, width=2)
        draw.line((53, 22, 547, 22), fill=ink, width=1)
        draw.line((53, 78, 547, 78), fill=ink, width=1)
        for x in (37, 562):
            draw.polygon([(x, 43), (x+7, 50), (x, 57), (x-7, 50)], fill=ink)
        draw.text((300, 50), title, fill=ink, font=font, anchor="mm")
        image.save(DEST / (key + ".png"), optimize=True)


if __name__ == "__main__":
    build()
