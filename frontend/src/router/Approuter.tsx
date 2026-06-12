import { BrowserRouter, Routes, Route } from "react-router-dom";
import ProductsPage from "@/products/page";
import ReservationPage from "../reservations/[id]/page";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ProductsPage />} />
        <Route path="/reservations/:id" element={<ReservationPage />} />
      </Routes>
    </BrowserRouter>
  );
}