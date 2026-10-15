import React from 'react'
import MusicBanner from '../components/banner';
import HitMusic from '../components/hits';
import { Separator } from '@/components/ui/separator';
import NewTrending from '../components/new-trending';

export default function MusicView() {
  return (
    <div>
      <MusicBanner />
      <HitMusic />
      <Separator />
      <NewTrending />
    </div>
  );
}

