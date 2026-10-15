"use client";

import PaymentButton from "./PaymentButton";

export default function PremiumTrialBanner() {
  return (
    <section className="text-center py-20 px-4 bg-white">
      <h1 className="text-4xl sm:text-5xl font-extrabold mb-4">
        Try Premium Now
      </h1>

      <p className="text-lg sm:text-xl text-gray-800 mb-6">
        Enjoy 1 month free trial • Then only 49,000 ₫/month • VAT not included • Cancel anytime
      </p>

      <PaymentButton />

      <div className="text-sm text-gray-500 mt-6">
        Recurring payment.{" "}
        <a
          href="#"
          className="text-blue-600 underline hover:text-blue-800"
        >
          Restrictions apply.
        </a>
      </div>
    </section>
  );
}
