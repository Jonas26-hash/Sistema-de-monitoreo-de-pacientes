import { Suspense, lazy } from 'react';

const InnerDotLottie = lazy(() =>
  import('@lottiefiles/dotlottie-react').then((m) => ({ default: m.DotLottieReact }))
);

interface LazyLottieProps {
  src: string;
  style?: React.CSSProperties;
  autoplay?: boolean;
  loop?: boolean;
}

export default function LazyLottie({ src, style, autoplay, loop }: LazyLottieProps) {
  return (
    <Suspense fallback={<div style={{ width: style?.width || 140, height: style?.height || 140 }} />}>
      <InnerDotLottie autoplay={autoplay} loop={loop} src={src} style={style} />
    </Suspense>
  );
}