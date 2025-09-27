import type { PropsWithChildren } from 'react';

export default function StatusPill({ status }: { status: 'QUEUED' | 'PROCESSING' | 'DONE' | 'FAILED' | 'UNKNOWN' }) {
    const cls =
        status === 'DONE' ? 'pill ok' :
            status === 'FAILED' ? 'pill err' :
                status === 'PROCESSING' || status === 'QUEUED' ? 'pill warn' :
                    'pill';
    return <span className={cls}>{status}</span>;
}
