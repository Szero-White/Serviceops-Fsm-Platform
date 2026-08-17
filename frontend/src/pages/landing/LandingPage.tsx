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
  OperationalScenariosSection,
} from './sections/MarketingSections'
import { DeploymentSection } from './sections/DeploymentSection'

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
        <OperationalScenariosSection />
        <DeploymentSection />
        <IntegrationsSection />
        <CtaBanner />
      </main>
      <LandingFooter />
    </div>
  )
}
