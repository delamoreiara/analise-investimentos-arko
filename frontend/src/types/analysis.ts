export type Veredito =
  | 'RECOMENDA'
  | 'RECOMENDADACOMCONDICAO'
  | 'NAORECOMENDA'
  | 'ELEGIVEL'
  | 'CONDICIONA'
  | 'APROVA'
  | 'REPROVA'
  | 'INDETERMINADO'
  | string

export interface Resultado {
  decisao: Veredito
  objetivo: string | null
  regrasAplicadas: string[]
  justificativas: string[]
  regrasNaoAvaliadas: string[]
}

export interface AnaliseDetalhada {
  id: string
  criadoEm: string
  origemExtracao: string
  dadosExtraidos: Record<string, string | number | boolean | null>
  alertas: string[]
  resultado: Resultado
  fundamentos: Array<Record<string, string | number | boolean | null>>
  rentabilidade: Array<Record<string, string | number | boolean | null>>
  dividendYield12m: number | null
  ativoEncontrado: boolean
  mensagem: string
}
