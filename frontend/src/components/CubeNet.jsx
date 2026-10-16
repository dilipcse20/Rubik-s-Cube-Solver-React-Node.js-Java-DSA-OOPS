import CubeFace from './CubeFace.jsx';

// The six faces drawn as a flat "net":
//
//        U
//     L  F  R  B
//        D
const FACE_NAMES = ['U', 'L', 'F', 'R', 'B', 'D'];

// "uncertainStickers" looks like [{ face: 'F', index: 3 }, ...]
function CubeNet({ state, onStickerClick, uncertainStickers }) {
  if (!state) {
    return <p className="placeholder-text">Waiting for the backend...</p>;
  }

  return (
    <div className="cube-net">
      {FACE_NAMES.map((name) => (
        <CubeFace
          key={name}
          name={name}
          stickers={state[name]}
          onStickerClick={onStickerClick}
          uncertain={uncertainStickers.filter((s) => s.face === name).map((s) => s.index)}
        />
      ))}
    </div>
  );
}

export default CubeNet;
