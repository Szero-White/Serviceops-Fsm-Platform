import './styles/landing.css'
import { CtaBanner } from './sections/CtaBanner'
import { LandingFooter } from './sections/LandingFooter'
import { HeroSection, LogoBar } from './sections/HeroSection'
import { LandingNavbar } from './sections/LandingNavbar'
import {
  FeaturesSection,
  HowItWorksSection,
  IntegrationsSection,
  StatsSection,
  TestimonialsSection,
} from './sections/MarketingSections'
import { PricingSection } from './sections/PricingSection'

export function LandingPage() {
  return (
    <div className="lp-root">
      <LandingNavbar />
      <main>
        <HeroSection />
        <LogoBar />
        <StatsSection />
        <FeaturesSection />
        <HowItWorksSection />
        <TestimonialsSection />
        <PricingSection />
        <IntegrationsSection />
        <CtaBanner />
      </main>
      <LandingFooter />
    </div>
  )
}
