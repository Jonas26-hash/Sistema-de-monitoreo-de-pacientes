export const EMVCO_PREFIX = '000201010212393232226f5086575b459e5884448a1a7219520456115303604';
export const EMVCO_SUFFIX = '5802PE5906YAPERO6004Lima';
export const YAPE_NUMERO = '930563938';

export function crc16ccitt(data: string): string {
  let crc = 0xFFFF;
  const poly = 0x1021;
  for (let i = 0; i < data.length; i++) {
    crc ^= (data.charCodeAt(i) << 8);
    for (let j = 0; j < 8; j++) {
      if (crc & 0x8000) {
        crc = ((crc << 1) ^ poly) & 0xFFFF;
      } else {
        crc = (crc << 1) & 0xFFFF;
      }
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, '0');
}

export function buildEmvcoPayload(amountCents: number): string {
  const amtStr = String(amountCents);
  const amtLen = String(amtStr.length).padStart(2, '0');
  const amtTag = `54${amtLen}${amtStr}`;
  const preCrc = EMVCO_PREFIX + amtTag + EMVCO_SUFFIX + '6304';
  return preCrc + crc16ccitt(preCrc);
}

export function isMobileDevice(): boolean {
  const uaMobile = /Android|iPhone|iPad|iPod|webOS|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  const narrow = window.innerWidth < 768;
  return uaMobile || narrow;
}

export function buildYapeDeepLink(emvcoPayload: string): string {
  return `yape://pago/${encodeURIComponent(emvcoPayload)}`;
}
