import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import CheckoutPage from './pages/CheckoutPage';
import PaymentPage from './pages/PaymentPage';
import PaymentConfirmationPage from './pages/PaymentConfirmationPage';
import PaymentSuccessPage from './pages/PaymentSuccessPage';
import SiteNavbar from './components/SiteNavbar';
import SiteFooter from './components/SiteFooter';

function App() {
    return (
        <BrowserRouter>
            <SiteNavbar />
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/checkout" element={<CheckoutPage />} />
                <Route path="/payment/:paymentId" element={<PaymentPage />} />
                <Route path="/payment-confirmation/:paymentId" element={<PaymentConfirmationPage />} />
                <Route path="/payment-success/:paymentId" element={<PaymentSuccessPage />} />
            </Routes>
            <SiteFooter />
        </BrowserRouter>
    );
}

export default App;
