// Pick a colour, then click stickers on the cube to paint them.
// Use it to fix a colour the photo reader got wrong, or to type in your own cube.
// Click the selected colour again to stop painting. Centre stickers never change.
const COLOURS = [
  { letter: 'U', name: 'White' },
  { letter: 'D', name: 'Yellow' },
  { letter: 'F', name: 'Green' },
  { letter: 'B', name: 'Blue' },
  { letter: 'L', name: 'Orange' },
  { letter: 'R', name: 'Red' },
];

function ColorPalette({ selected, onSelect }) {
  return (
    <div className="palette">
      <p className="palette-title">
        {selected ? 'Click a sticker to paint it.' : 'Pick a colour to paint stickers:'}
      </p>
      <div className="palette-colours">
        {COLOURS.map((colour) => (
          <button
            key={colour.letter}
            type="button"
            title={colour.name}
            className={`palette-button sticker-${colour.letter} ${selected === colour.letter ? 'palette-selected' : ''}`}
            onClick={() => onSelect(selected === colour.letter ? null : colour.letter)}
          >
            {colour.name}
          </button>
        ))}
      </div>
    </div>
  );
}

export default ColorPalette;
