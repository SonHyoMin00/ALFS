"use client";

import React, { useState, useEffect } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { Tooltip } from "@chakra-ui/react";
import Link from "next/link";
import AddToCartFromList from "../modal/AddToCartFromList";
import { BsShieldFillCheck, BsShieldFillExclamation, BsShieldFillMinus, BsShieldFillX } from "react-icons/bs";
type CardProps = {
  name: string;
  image: string;
  id: number;
  title: string;
  price: number;
  sale: number;
  filterCode: Array<number>;
  hates: Array<string>;
  allergies: Array<string>;
  isSpecial: boolean;
};

export default function Card({
  name,
  image,
  id,
  title,
  price,
  sale,
  filterCode,
  hates,
  allergies,
  isSpecial,
}: CardProps) {
  const formattedSale = new Intl.NumberFormat().format(sale);
  const formattedPrice = new Intl.NumberFormat().format(price);
  const discount = Math.round(((price - sale) / price) * 100);
  const router = useRouter();
  const [filter, setFilter] = useState<Array<number>>([0, 0, 0, 0]);
  const [filtered, setFiltered] = useState<boolean>(false);
  const moveDetail = (id: number, image: string) => {
    const existingDataString = sessionStorage.getItem("productId") || "[]";
    const existingData = JSON.parse(existingDataString);
    const isIdExists = existingData.some((item: any) => item.id === id);
    const lastItemIndex = existingData.length > 0 ? existingData[0].index : null;
    if (!isIdExists || (lastItemIndex !== null && Math.abs(lastItemIndex - id) >= 1)) {
      existingData.unshift({ id, image, index: id });
      sessionStorage.setItem("productId", JSON.stringify(existingData));
    }

    router.push(`/detail/${id}`);
  };
  useEffect(() => {
    const updatedFilter = [...filter];
    filterCode.forEach((index) => {
      updatedFilter[index] = 1;
    });
    setFilter(updatedFilter);
    setFiltered(true);
  }, []);
  return (
    <div className="w-[178px] h-[450px]">
      <Link href={{ pathname: `/detail/${id}` }}>
        <Image
          src={image}
          width={178}
          height={240}
          className={`${id}`}
          alt="product image"
          onClick={() => {
            moveDetail(id, image);
          }}
        />
      </Link>
      <div className="AddToCartFromList">
        <AddToCartFromList id={id} image={image} name={name} price={price} sale={sale} isSpecial={isSpecial} />
      </div>

      <Link href={{ pathname: `/detail/${id}` }}>
        <div className="CardFooter w-[178px] h-[80px] mt-[4px]">
          <div className="flex">
            {filtered && filter[3] === 1 && (
              <Tooltip label="안전합니다!" placement="top">
                <span className="mr-[5px]">
                  <BsShieldFillCheck style={{ fontSize: "20px", color: "#008000" }} />
                </span>
              </Tooltip>
            )}
            {filtered && filter[0] === 1 && (
              <Tooltip label={`${hates}가 포함되어있습니다.`} placement="top">
                <span className="mr-[5px]" style={{ fontSize: "20px", color: "#ffff00" }}>
                  <BsShieldFillExclamation />
                </span>
              </Tooltip>
            )}
            {filtered && filter[2] === 1 && (
              <Tooltip label="제조시설 알러지가 포함되어있습니다." placement="top">
                <span className="mr-[5px]" style={{ fontSize: "20px", color: "#ff8c00" }}>
                  <BsShieldFillMinus />
                </span>
              </Tooltip>
            )}
            {filtered && filter[1] === 1 && (
              <Tooltip label={`${allergies}가 포함되어있습니다.`} placement="top">
                <span className="mr-[5px]" style={{ fontSize: "20px", color: "#ff0000" }}>
                  <BsShieldFillX />
                </span>
              </Tooltip>
            )}
          </div>
          <div className="CardTitle w-[full] text-[13px] mt-[5px]">{name}</div>
          <div className="CardSubtitle w-[full] opacity-[0.3] mt-[7px] text-[10px]">{title}</div>
          {price !== sale && (
            <div className="Cost w-[full] h-[14px] mt-[7px] text-[13px] opacity-[0.3] line-through">
              {formattedPrice}원
            </div>
          )}
          <div className="CardPrice w-[full] text-[14px] font-bold mt-[9px]">
            {discount !== 0 && (
              <span className="Discount mr-[5px]" style={{ color: "red" }}>
                {discount}%
              </span>
            )}
            {formattedSale}원
          </div>
        </div>
      </Link>
    </div>
  );
}
