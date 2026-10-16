import { ALGORITHM_INFO } from '../data/algorithmInfo.js';

// Bottom section: explains the algorithm currently selected in the controls.
function AlgorithmInfo({ algorithm }) {
  const info = ALGORITHM_INFO[algorithm];

  if (!info) {
    return null;
  }

  return (
    <section className="card algorithm-info">
      <h2>Algorithm Explanation</h2>
      <h3>{info.name}</h3>

      <div className="info-grid">
        <div>
          <h4>How it works</h4>
          <ol>
            {info.howItWorks.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </div>

        <div>
          <h4>Complexity</h4>
          <p>
            <strong>Time:</strong> {info.time}
          </p>
          <p>
            <strong>Space:</strong> {info.space}
          </p>
        </div>

        <div>
          <h4>Advantages</h4>
          <ul>
            {info.advantages.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>

        <div>
          <h4>Limitations</h4>
          <ul>
            {info.limitations.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

export default AlgorithmInfo;
