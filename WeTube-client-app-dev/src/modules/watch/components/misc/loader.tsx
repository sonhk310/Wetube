import { Spinner } from "@/components/ui/spinner";

export const Loader = () => {
  return (
    <div className="w-full flex justify-center items-center py-4">
      <Spinner className="size-8" />
    </div>
  );
};
