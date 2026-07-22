export interface Word {
  id: number
  word: string
  classifier: string
  category: string
  image_url: string | null
  distractors: string[]
  created_at: string
}

export interface WordFormData {
  word: string
  classifier: string
  category: string
  image_url?: string | null
  distractors: string[]
}

export type CategoryFilter = string | 'all'
