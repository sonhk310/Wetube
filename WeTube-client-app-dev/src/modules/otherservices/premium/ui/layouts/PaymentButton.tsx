"use client";

import { useAuth } from '@/contexts/auth-context';
import React, { useState } from 'react'

export default function PaymentButton() {
  const [loading, setLoading] = useState(false);
  const {user}=useAuth();

  const handlePayment = async () => {
    setLoading(true);
    try {
      const res = await fetch(
        "https://wetube.name.vn/api/payment/momo/create",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            userId: user?.sub,
            subPackId: "550e8400-e29b-41d4-a716-446655440000",
          }),
        }
      );

      if (!res.ok) throw new Error("Payment API failed");

      const data = await res.json();
      console.log("MoMo response:", data);

      if (data.payUrl) {
        window.location.href = data.payUrl;
      } else {
        alert("Did not receive payment link from MoMo");
      }
    } catch (err) {
      console.error(err);
      alert("An error occurred while creating the payment.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handlePayment}
      disabled={loading}
      className="bg-blue-600 text-white text-lg font-medium px-6 py-3 rounded-full hover:bg-blue-700 transition disabled:opacity-50"
    >
      {loading ? "Processing..." : "Try it for 1 month 0 VND"}
    </button>
  );
}
