// src/components/Table.tsx
import type { PropsWithChildren } from 'react';

export function Table(p: PropsWithChildren) { return <table>{p.children}</table>; }
export function Thead(p: PropsWithChildren) { return <thead>{p.children}</thead>; }
export function Tbody(p: PropsWithChildren) { return <tbody>{p.children}</tbody>; }
export function Tr(p: PropsWithChildren) { return <tr>{p.children}</tr>; }
export function Th(
  p: PropsWithChildren & { sortable?: boolean; onClick?: () => void }
) {
  return (
    <th className={p.sortable ? 'sortable' : undefined} onClick={p.onClick}>
      {p.children}
    </th>
  );
}
export function Td(p: PropsWithChildren) { return <td>{p.children}</td>; }
