//! The LoneStar 99 Texas emblem. The 40×32 one-bit bitmap is derived from the
//! project's LS-99 brand artwork, reduced to the TI console's 5×4 character-cell
//! area. The cyan/black TMS9918A palette supplies its display colors; its
//! text-first title alongside it identifies the product and DeveloperCal.
//!
//! [`packed`] packs the bitmap into twenty 8×8 glyphs (row-major cell order);
//! [`emit_gpl_bytes`] renders them for GROM >1600. `console.gpl` places these
//! glyphs on the title and selection screens.

// Modified MIT License
//
// Copyright (c) 2026 Joel Odom
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, and sublicense copies of the
// Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, subject to
// the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, "Sell" means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or other
// consideration (including without limitation fees for hosting or consulting/
// support services related to the Software), a product or service whose value
// derives, entirely or substantially, from the functionality of the Software. Any
// license notice or attribution required by the License must also include this
// Commons Clause License Condition notice.
//
// Software: Libre99
//
// License: Modified MIT
//
// Licensor: Joel Odom
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

//! The LoneStar 99 Texas emblem. Its 40×32 one-bit bitmap is derived from the
//! project's LS-99 brand artwork, reduced to the TI console's 5×4 character-cell
//! area. The TMS9918A palette supplies its cyan/black display colors.
//!
//! [`packed`] packs this bitmap into twenty 8×8 glyphs (row-major cell order);
//! [`emit_gpl_bytes`] emits them for GROM >1600. `console.gpl` places these glyphs
//! on the title and selection screens. The upstream file copyright and license
//! remain above; the generated artwork is an LS-99 project asset.

/// The emblem's size in 8×8 characters (columns × rows).
pub const COLS: usize = 5;
pub const ROWS: usize = 4;
/// Pixel dimensions of the emblem canvas (COLS·8 × ROWS·8).
pub const WIDTH: usize = COLS * 8;
pub const HEIGHT: usize = ROWS * 8;
/// GROM address the emblem's glyphs are spliced at (>1600..>169F).
pub const BASE: u16 = 0x1600;
/// First character code used by the emblem (>0B..>1E).
pub const FIRST_CODE: u8 = 0x0B;
/// Number of glyphs (one per cell of the COLS×ROWS block).
pub const GLYPHS: usize = COLS * ROWS;

/// One-bit Texas silhouette sampled from `app/src/main/res/drawable-nodpi/ls99_brand.png`.
const TEXAS: [&str; HEIGHT] = [
    ".........##########.....................",
    ".........##########.....................",
    ".........##########.....................",
    ".........##########.....................",
    ".........##########.....................",
    ".........######...#####..#########......",
    ".........#####....##################....",
    ".........#####....######..........###...",
    ".........#####...######..........####...",
    ".........####....######....##########...",
    ".........####....#####...........####...",
    ".........####...#######..........###....",
    "############....#############....####...",
    ".###########.........#..........######..",
    "..##########.......##..........########.",
    "...##############.......##.......#######",
    "....############........#........#######",
    ".....##########...##...#...##...########",
    "......#########........#........########",
    "......#############....#####....########",
    ".......#####..###.#...###..#...######...",
    "........###....#.....##.......#####.....",
    "................##################......",
    "................################........",
    ".................#############..........",
    "..................###########...........",
    "...................#########............",
    "...................#########............",
    "....................########............",
    ".....................#######............",
    ".......................######...........",
    ".........................####...........",
];

/// The emblem as a `HEIGHT × WIDTH` boolean grid (`true` = ink).
pub fn bitmap() -> Vec<Vec<bool>> {
    TEXAS.iter()
        .map(|row| row.bytes().map(|pixel| pixel == b'#').collect())
        .collect()
}

/// Pack the emblem into the [`GLYPHS`] 8×8 glyphs, row-major over the COLS×ROWS
/// block: cell `(gr, gc)` → glyph index `COLS*gr + gc`, covering pixel rows
/// `8*gr..` and cols `8*gc..`. Returns `GLYPHS*8` bytes (glyph 0 first, one row
/// per byte, MSB = leftmost pixel).
pub fn packed() -> Vec<u8> {
    let g = bitmap();
    let mut out = Vec::with_capacity(GLYPHS * 8);
    for gr in 0..ROWS {
        for gc in 0..COLS {
            for r in 0..8 {
                let mut b = 0u8;
                for c in 0..8 {
                    if g[gr * 8 + r][gc * 8 + c] {
                        b |= 0x80u8 >> c;
                    }
                }
                out.push(b);
            }
        }
    }
    out
}

/// The emblem's pixel rows as string-art (`#`/space), for eyeballing.
pub fn art_lines() -> Vec<String> {
    bitmap()
        .iter()
        .map(|row| row.iter().map(|&p| if p { '#' } else { ' ' }).collect())
        .collect()
}

/// Render the emblem as a GPL `BYTE` block (a `GROM >1600` origin, one glyph per
/// line) for splicing into GPL source. `label` names the first glyph's address.
pub fn emit_gpl_bytes(label: &str) -> String {
    let bytes = packed();
    let mut s = format!("        GROM >{BASE:04X}\n");
    for g in 0..GLYPHS {
        let glyph = &bytes[g * 8..g * 8 + 8];
        let hex: Vec<String> = glyph.iter().map(|b| format!(">{b:02X}")).collect();
        let lbl = if g == 0 { label } else { "" };
        s.push_str(&format!(
            "{lbl:<7} BYTE {}   ; LS-99 glyph >{:02X}\n",
            hex.join(","),
            FIRST_CODE as usize + g
        ));
    }
    s
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn packs_the_expected_number_of_glyphs() {
        assert_eq!(packed().len(), GLYPHS * 8);
        assert_eq!(GLYPHS, 20);
    }

    #[test]
    fn transparent_margin_cells_are_preserved() {
        let bytes = packed();
        assert!(bytes[..8].iter().all(|&b| b == 0), "top-left transparent margin changed");
        assert!(bytes.iter().any(|&b| b != 0), "Texas silhouette must contain ink");
        assert!(bytes.contains(&0), "emblem should retain transparent pixels");
    }

    #[test]
    fn codes_stay_below_the_font() {
        // The glyphs must fit between the copyright glyph (>0A) and the font (>20).
        const { assert!(FIRST_CODE > 0x0A) };
        assert!(FIRST_CODE as usize + GLYPHS - 1 < 0x20);
    }

    #[test]
    fn sampled_texas_art_fills_the_emblem_canvas() {
        let g = bitmap();
        assert_eq!(g.len(), HEIGHT);
        assert!(g.iter().all(|row| row.len() == WIDTH));
        assert!(g.iter().flatten().any(|&pixel| pixel));
        assert!(g.iter().flatten().any(|&pixel| !pixel));
        assert!(g[0][9]);
        assert!(g[HEIGHT - 1][25]);
        assert!(g[12][0]);
        assert!(g[15][WIDTH - 1]);
    }

}
