export default function DataLoading({ height = 300 }: { height?: number }) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: height,
      width: '100%',
      gap: 16,
    }}>
      <div style={{ width: 120, height: 80, position: 'relative' }}>
        <img src="/lottie/Load.svg" alt="Cargando datos" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
      </div>
      <span style={{
        color: 'var(--text-muted)', fontSize: 13, fontWeight: 500,
      }}>
        Cargando datos...
      </span>
    </div>
  );
}
