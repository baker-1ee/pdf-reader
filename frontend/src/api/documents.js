// API 엔드포인트 설정
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// PDF 텍스트 추출 API
export const extractText = async (file) => {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE_URL}/api/documents/extract`, {
        method: 'POST',
        body: formData
    })

    if (!response.ok) {
        throw new Error('텍스트 추출 실패')
    }

    return response.json()
}
