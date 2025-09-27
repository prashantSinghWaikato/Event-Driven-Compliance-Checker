export default function RiskBadge({ score }: { score: number }) {
  const level = score >= 80 ? 'high' : score >= 50 ? 'med' : 'low';
  const label = score >= 80 ? 'High' : score >= 50 ? 'Medium' : 'Low';
  return <span className={`badge ${level}`}>{label} ({score})</span>;
}