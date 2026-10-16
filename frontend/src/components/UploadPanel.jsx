import { useState } from 'react';

import { uploadPhotos, uploadStateFile } from '../api/cubeApi.js';

// How to hold the cube for each photo. The photo order is U, R, F, D, L, B.
const PHOTO_FACES = [
  { face: 'U', hint: 'Top face. Photo taken from above, the Back side at the top of the photo.' },
  { face: 'R', hint: 'Right face. The Up side at the top of the photo.' },
  { face: 'F', hint: 'Front face. The Up side at the top of the photo.' },
  { face: 'D', hint: 'Bottom face. The Front side at the top of the photo.' },
  { face: 'L', hint: 'Left face. The Up side at the top of the photo.' },
  { face: 'B', hint: 'Back face. The Up side at the top of the photo.' },
];

// Lets the user load their own cube: 6 photos, or a text/JSON file.
// When the backend answers, it calls onLoaded(result) so App can show the cube.
function UploadPanel({ isOnline, onLoaded, onError }) {
  const [photos, setPhotos] = useState({});
  const [isWorking, setIsWorking] = useState(false);

  const allPhotosChosen = PHOTO_FACES.every(({ face }) => photos[face]);

  function handlePhotoChosen(face, event) {
    const file = event.target.files[0];
    setPhotos((current) => ({ ...current, [face]: file }));
  }

  // Runs an upload and passes the result (or the error) to App.
  async function runUpload(upload) {
    setIsWorking(true);
    try {
      onLoaded(await upload());
    } catch (error) {
      onError(error.message);
    } finally {
      setIsWorking(false);
    }
  }

  function handleDetect() {
    runUpload(() => uploadPhotos(photos));
  }

  function handleFileChosen(event) {
    const file = event.target.files[0];
    event.target.value = '';
    if (file) {
      runUpload(() => uploadStateFile(file));
    }
  }

  return (
    <section className="card">
      <h2>Upload Your Cube</h2>

      <h3>Option 1: six photos</h3>
      <p className="hint">
        Take one clear, straight-on photo of each face in good light. The photo is cut to
        its middle square, so keep the face centred. The centre sticker names each colour,
        so any colour scheme works.
      </p>

      <div className="photo-grid">
        {PHOTO_FACES.map(({ face, hint }) => (
          <label key={face} className="photo-field" title={hint}>
            <span>
              <strong>{face}</strong> {photos[face] ? photos[face].name : 'choose photo'}
            </span>
            <input type="file" accept="image/png,image/jpeg" onChange={(e) => handlePhotoChosen(face, e)} />
          </label>
        ))}
      </div>

      <ul className="hint photo-hints">
        {PHOTO_FACES.map(({ face, hint }) => (
          <li key={face}>
            <strong>{face}:</strong> {hint}
          </li>
        ))}
      </ul>

      <button
        type="button"
        className="primary-button full-width"
        onClick={handleDetect}
        disabled={!allPhotosChosen || isWorking || !isOnline}
      >
        {isWorking ? 'Reading...' : 'Detect Colours From Photos'}
      </button>

      <h3>Option 2: a text or JSON file</h3>
      <p className="hint">
        A .txt file with 54 letters (U, R, F, D, L, B) in the order U R F D L B, or a .json
        file like the one the API returns.
      </p>
      <label className="file-button">
        Choose cube file
        <input type="file" accept=".txt,.json,text/plain,application/json" onChange={handleFileChosen} disabled={isWorking || !isOnline} />
      </label>
    </section>
  );
}

export default UploadPanel;
