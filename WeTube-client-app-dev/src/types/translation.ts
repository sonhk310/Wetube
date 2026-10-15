export interface TranslationSegment {
  start: number;           
  end: number;
  originalText: string;
  translatedText: string;
}

export interface TranslationDto {
  translationId: string;
  targetLang: string;
  subtitleUrl: string;      
  audioUrl?: string;            
  status: "pending" | "in_progress" | "completed" | "failed";
  segments?: TranslationSegment[];
  createdAt: string;
  updatedAt: string;
}