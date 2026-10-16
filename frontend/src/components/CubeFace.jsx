// One face of the cube: a label and nine coloured stickers.
// "stickers" is a 9-letter text such as "UUFUUFUUF". Each letter picks a colour.
//
// Props:
//   name           the face letter (U, D, F, B, L or R)
//   stickers       the 9 letters
//   onStickerClick called with (faceName, stickerNumber) when a sticker is clicked
//   uncertain      list of sticker numbers the photo reader was not sure about
function CubeFace({ name, stickers, onStickerClick, uncertain }) {
  const letters = stickers.split('');

  return (
    <div className="cube-face" style={{ gridArea: name }}>
      <span className="face-label">{name}</span>
      <div className="sticker-grid">
        {letters.map((letter, index) => {
          let className = `sticker sticker-${letter}`;
          if (uncertain.includes(index)) {
            className += ' sticker-uncertain';
          }
          return (
            <button
              key={index}
              type="button"
              className={className}
              onClick={() => onStickerClick(name, index)}
              aria-label={`${name} sticker ${index + 1}`}
            />
          );
        })}
      </div>
    </div>
  );
}

export default CubeFace;
