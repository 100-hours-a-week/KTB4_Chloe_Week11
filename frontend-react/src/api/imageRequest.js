const BASE_URL = import.meta.env.VITE_API_URL;

function getImageUrl(type, filename) {
  if (!filename) return null;

  // 이미 완전한 URL(S3 등)이면 그대로 반환
  if (filename.startsWith('http://') || filename.startsWith('https://')) {
    return filename;
  }
  
  return `${BASE_URL}/images/${type}/${filename}`;
}

export function getProfileImageUrl(filename) {
  return getImageUrl('profile', filename);
}

export function getPostImageUrl(filename) {
  return getImageUrl('post', filename);
}
