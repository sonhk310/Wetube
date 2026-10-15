import FormSection from '../sections/form-section';

interface VideoViewPageProps {
  readonly videoId: string;
}

export default function VideoView({ videoId }: VideoViewPageProps) {
    return (
      <div className="px-4 pt-2.5 max-w-screen-xl mx-auto">
        <FormSection videoId={videoId} />
      </div>
    );
}
