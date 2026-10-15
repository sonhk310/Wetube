import FeatureMusic from "../layouts/animasion/featureMusic";
import FeatureNoAds from "../layouts/animasion/featureNoads";
import FeatureOffline from "../layouts/animasion/featureoffline";
import PremiumFaq from "../layouts/premium-faq";
import PremiumTryitforfree from "../layouts/premium-Tryitforfree";
import PremiumFeatures from "../layouts/premiumFeature";
import PremiumTrialBanner from "../layouts/premiumTrialBanner";

// app/premium/page.tsx
export default function PremiumView() {
  return (
    <section className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex flex-col items-center text-center px-4 py-20">
      <PremiumTryitforfree/>
      <PremiumFeatures />

      <FeatureNoAds/>
      <FeatureOffline/>
      <FeatureMusic/>

      <PremiumTrialBanner/>
      <PremiumFaq/>
    </section>
  );
}
