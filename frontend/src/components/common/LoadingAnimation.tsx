export default function LoadingAnimation() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--bg-layout)',
      gap: 32,
    }}>
      <div style={{ width: 200, height: 200, position: 'relative' }}>
        <div style={{
          position: 'absolute', inset: 0,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(0,212,170,0.12) 0%, transparent 70%)',
          animation: 'loading-pulse 2s ease-in-out infinite',
        }} />
        <img src="/lottie/Loading.svg" alt="Cargando" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
      </div>
      <div className="loading-dots">
        <span className="dot" />
        <span className="dot" />
        <span className="dot" />
      </div>
      <span style={{
        color: 'var(--text-muted)', fontSize: 14, fontWeight: 500,
        letterSpacing: '0.3px', marginTop: -8,
      }}>
        Cargando
      </span>
      <style>{`
        @keyframes loading-pulse {
          0%, 100% { transform: scale(0.95); opacity: 0.6; }
          50% { transform: scale(1.05); opacity: 1; }
        }
        .loading-dots { display: flex; gap: 8px; }
        .dot {
          width: 8px; height: 8px; border-radius: 50%;
          background: #00D4AA;
          animation: dot-bounce 1.4s ease-in-out infinite both;
        }
        .dot:nth-child(2) { animation-delay: 0.16s; }
        .dot:nth-child(3) { animation-delay: 0.32s; }
        @keyframes dot-bounce {
          0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
          40% { transform: scale(1); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
