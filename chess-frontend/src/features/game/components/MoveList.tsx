import { Card } from '../../../shared/ui/Card'

function chunkPairs(moves: { ply: number; san?: string | null; uci?: string | null }[]) {
  const rows: { no: number; white?: string; black?: string }[] = []
  for (let i = 0; i < moves.length; i += 2) {
    const no = Math.floor(i / 2) + 1
    rows.push({
      no,
      white: moves[i]?.san || moves[i]?.uci || '',
      black: moves[i + 1]?.san || moves[i + 1]?.uci || '',
    })
  }
  return rows
}

export function MoveList({
  moves,
}: {
  moves: { ply: number; san?: string | null; uci?: string | null }[]
}) {
  const rows = chunkPairs(moves)
  return (
    <Card>
      <div className="text-xs text-slate-400">Moves</div>
      <div className="mt-3 max-h-[420px] overflow-auto text-sm">
        <table className="w-full">
          <tbody>
            {rows.map((pair) => (
              <tr key={pair.no} className="border-b border-slate-800/60">
                <td className="py-2 pr-2 text-slate-500 w-10">{pair.no}.</td>
                <td className="py-2 pr-2">{pair.white ?? ''}</td>
                <td className="py-2">{pair.black ?? ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
