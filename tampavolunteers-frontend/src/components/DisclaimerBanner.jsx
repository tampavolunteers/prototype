import React from 'react';

const DisclaimerBanner = () => {
  return (
    <div
      role="alert"
      className="bg-yellow-100 border-b border-yellow-300 text-yellow-800 px-4 py-2 text-center text-sm"
    >
      <strong className="font-semibold">Proof of concept:</strong> This site is a demo and is not live in
      production yet. Data may be reset at any time.
    </div>
  );
};

export default DisclaimerBanner;
