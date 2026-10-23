import { Suspense } from "react";
import PaymentResult from "@/modules/otherservices/premium/ui/layouts/payment-result";

export default function PaymentResultPage() {
  return (
    <Suspense fallback={null}>
      <PaymentResult />
    </Suspense>
  );
}
